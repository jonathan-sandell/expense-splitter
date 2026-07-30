package com.jonathansandell.expense_splitter.service;

import com.jonathansandell.expense_splitter.entity.Group;
import com.jonathansandell.expense_splitter.repository.GroupRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class GroupService {

    private final GroupRepository groupRepository;

    public GroupService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    public Group createGroup(String name) {
        return groupRepository.save(new Group(name));
    }

    public Group getGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("No group with id " + groupId));
    }
}
