package com.jonathansandell.expense_splitter.service;

import com.jonathansandell.expense_splitter.entity.Group;
import com.jonathansandell.expense_splitter.entity.Member;
import com.jonathansandell.expense_splitter.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final GroupService groupService;

    public MemberService(MemberRepository memberRepository, GroupService groupService) {
        this.memberRepository = memberRepository;
        this.groupService = groupService;
    }

    public Member addMember(Long groupId, String name) {
        Group group = groupService.getGroup(groupId);
        return memberRepository.save(new Member(name, group));
    }

    public List<Member> listMembers(Long groupId) {
        return memberRepository.findByGroupId(groupId);
    }
}
