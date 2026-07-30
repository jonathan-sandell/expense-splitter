package com.jonathansandell.expense_splitter.controller;

import com.jonathansandell.expense_splitter.dto.AddMemberRequest;
import com.jonathansandell.expense_splitter.dto.MemberResponse;
import com.jonathansandell.expense_splitter.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse addMember(@PathVariable Long groupId, @Valid @RequestBody AddMemberRequest request) {
        return MemberResponse.from(memberService.addMember(groupId, request.name()));
    }

    @GetMapping
    public List<MemberResponse> listMembers(@PathVariable Long groupId) {
        return memberService.listMembers(groupId).stream().map(MemberResponse::from).toList();
    }
}
