package vn.edu.ute.milktea.repository.branch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.branch.Branch;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    Optional<Branch> findByCode(String code);

    List<Branch> findAllByStatusOrderByCodeAsc(String status);

    default List<Branch> findByActiveTrueOrderByIdAsc() {
        return findAllByStatusOrderByCodeAsc("HOAT_DONG");
    }
}
