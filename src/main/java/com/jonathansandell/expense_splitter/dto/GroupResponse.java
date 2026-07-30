package com.jonathansandell.expense_splitter.dto;

import com.jonathansandell.expense_splitter.entity.Group;

public record GroupResponse(Long id, String name) {

    public static GroupResponse from(Group group) {
        return new GroupResponse(group.getId(), group.getName());
    }
}
