package az.iktlab.last.project.doctorbookingtgbot.auth.dao.repository;

import az.iktlab.last.project.doctorbookingtgbot.auth.model.ERole;
import az.iktlab.last.project.doctorbookingtgbot.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(ERole name);
}