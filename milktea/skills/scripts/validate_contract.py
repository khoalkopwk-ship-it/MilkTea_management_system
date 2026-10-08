#!/usr/bin/env python3
"""Validate this design bundle. Does NOT execute the MilkTea application.
Run: python3 scripts/validate_contract.py
Dependency: PyYAML (python3 -m pip install pyyaml), no network needed at runtime.
"""
from pathlib import Path
import json,re,sys
try:
 import yaml
except ImportError:
 sys.exit('Thiếu PyYAML: cài python3 -m pip install pyyaml rồi chạy lại.')
ROOT=Path(__file__).resolve().parents[1]
errors=[]
def check(condition,message):
 if not condition:errors.append(message)
class UniqueLoader(yaml.SafeLoader):pass
def mapping(loader,node,deep=False):
 result={}
 for key,value in node.value:
  k=loader.construct_object(key,deep=deep)
  if k in result:raise ValueError('Duplicate YAML key: '+str(k))
  result[k]=loader.construct_object(value,deep=deep)
 return result
UniqueLoader.add_constructor(yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG,mapping)
spec=yaml.load((ROOT/'milktea-openapi.yaml').read_text(),Loader=UniqueLoader)
check(spec['openapi']=='3.1.0','OpenAPI version')
check(spec['info']['version']=='2.0.0','Contract version')
schemas=spec['components']['schemas']
refs=[]
def walk(value):
 if isinstance(value,dict):
  if '$ref' in value:refs.append(value['$ref'])
  for x in value.values():walk(x)
 elif isinstance(value,list):
  for x in value:walk(x)
walk(spec)
for ref in refs:
 check(ref.startswith('#/'),'Unexpected external schema ref '+ref)
 if not ref.startswith('#/'):continue
 cur=spec
 try:
  for part in ref[2:].split('/'):cur=cur[part.replace('~1','/').replace('~0','~')]
 except (KeyError,TypeError):check(False,'Unresolved ref '+ref)
ops=[];coverage=set()
for path,item in spec['paths'].items():
 check(path.startswith('/api/v1/'),'Wrong API prefix '+path)
 check('branch' not in path.lower(),'Branch API '+path)
 for method,op in item.items():
  if method not in ['get','post','put','delete','patch']:continue
  ops.append(op['operationId']);coverage.update(op.get('x-use-cases',[]))
  check(bool(op.get('x-use-cases')),'Missing UC '+path+' '+method)
  parameters=item.get('parameters',[])+op.get('parameters',[])
  for name in re.findall(r'\{([^}]+)\}',path):
   check(any(p.get('in')=='path' and p['name']==name and p.get('required') for p in parameters),'Missing path parameter '+path+' '+name)
  if method!='get':check(any(p.get('name')=='X-CSRF-TOKEN' and p.get('required') for p in parameters),'Missing CSRF '+path)
  for par in parameters:
   if par.get('name')=='Idempotency-Key':check(par['schema'].get('maxLength')==64,'Key length must match DB '+path)
  if path.startswith(('/api/v1/admin/','/api/v1/cashier/','/api/v1/kitchen/','/api/v1/inventory/')):
   check(bool(op.get('x-allowed-account-roles')),'Missing allowed staff roles '+path)
  if op.get('x-allowed-account-roles'):
   check(set(op['x-allowed-account-roles'])<=set(schemas['Role']['enum']),'Unknown role '+path)
  check(bool(op.get('responses')),'Missing responses '+path)
check(len(ops)==len(set(ops)),'Duplicate operationId')
check(coverage=={'UC%02d'%i for i in range(1,42)},'UC coverage must be UC01..41')
check(schemas['Role']['enum']==['ADMIN','CASHIER','KITCHEN','CUSTOMER'],'Four account roles only')
check(set(schemas['OrderStatus']['enum'])=={'CHO_XAC_NHAN','CHO_CHE_BIEN','DANG_CHE_BIEN','HOAN_THANH','DA_HUY'},'Five aggregate order states')
check(set(schemas['PaymentMethod']['enum'])=={'CASH','BANK_TRANSFER'},'Both payment methods')
check('status' not in schemas['OrderItemView']['properties'],'No per-item state')
check('status' not in schemas['TableWrite']['properties'],'Admin table CRUD cannot change occupancy')
check('threshold' not in schemas['MaterialWrite']['properties'],'Threshold belongs to Stock position')
check(schemas['Version']['type']=='string','Version must be string BIGINT')
check(schemas['CartView']['properties']['version']=={'$ref':'#/components/schemas/Version'},'Cart version contract')
check('session' in schemas['OrderCreatedView']['required'],'Create order must return session/cart')
for f in ['shopName','address','bankName','bankAccountNumber','bankAccountHolder','discountPercent']:
 check(f in schemas['SettingsWrite']['required'],'Missing Settings field '+f)
 check(f in schemas['PublicShopInfoView']['required'],'Missing public payment info '+f)
paths=spec['paths']
check(paths['/api/v1/kitchen/orders/{orderId}/complete']['post']['x-allowed-account-roles']==['KITCHEN'],'Only kitchen completes')
check(paths['/api/v1/cashier/table-sessions/{sessionId}/close']['post']['x-allowed-account-roles']==['CASHIER'],'Only cashier closes')
check(paths['/api/v1/inventory/issues']['post']['x-allowed-account-roles']==['KITCHEN'],'Kitchen issues without approval')
check(set(paths['/api/v1/inventory/waste']['post']['x-allowed-account-roles'])=={'KITCHEN','ADMIN'},'Waste UC34 permissions')
# Verify all relative Markdown links within the exported skill.
for file in ROOT.rglob('*.md'):
 t=file.read_text()
 check('ALOUTE' not in t and 'aloute-openapi' not in t,'Old project reference '+str(file))
 for target in re.findall(r'\[[^\]]+\]\(([^)]+)\)',t):
  if '://' in target or target.startswith('#'):continue
  target=target.split('#')[0]
  check((file.parent/target).exists(),'Broken link '+str(file.relative_to(ROOT))+' -> '+target)
ucs=re.findall(r'^## UC(\d{2}) ',(ROOT/'use_case_specifications.md').read_text(),re.M)
tests=re.findall(r'^## T(\d{2}) ',(ROOT/'acceptance_tests.md').read_text(),re.M)
check(ucs==['%02d'%i for i in range(1,42)],'41 ordered UC specifications')
check(tests==['%02d'%i for i in range(1,61)],'60 ordered acceptance cases')
# Validate sample wire values using schema patterns (this is not a full OpenAPI validator).
for schema,good,bad in [('Id','9007199254740993','1.5'),('Version','0','-1'),('Money','63000.00','1.123'),('Decimal','20.125','-1')]:
 check(re.fullmatch(schemas[schema]['pattern'],good) is not None,'Good example failed '+schema)
 check(re.fullmatch(schemas[schema]['pattern'],bad) is None,'Bad example passed '+schema)
if errors:
 print('\n'.join(errors));sys.exit(1)
print(json.dumps({'status':'passed','paths':len(paths),'operations':len(ops),'schemas':len(schemas),'refs':len(refs),'uc':len(ucs),'tests':len(tests),'checks':'Design contract/links/invariants only; application tests not executed.'},ensure_ascii=False))
