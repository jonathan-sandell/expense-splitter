package com.jonathansandell.expense_splitter.dto;

import com.jonathansandell.expense_splitter.entity.Member;

public record MemberResponse(Long id, String name, Long groupId) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getName(), member.getGroup().getId());
    }
}
