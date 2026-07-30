package com.jonathansandell.expense_splitter.repository;

import com.jonathansandell.expense_splitter.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {
}
