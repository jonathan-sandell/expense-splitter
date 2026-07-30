package com.jonathansandell.expense_splitter.service;

import com.jonathansandell.expense_splitter.dto.CreateExpenseRequest;
import com.jonathansandell.expense_splitter.entity.Expense;
import com.jonathansandell.expense_splitter.entity.Group;
import com.jonathansandell.expense_splitter.entity.Member;
import com.jonathansandell.expense_splitter.repository.ExpenseRepository;
import com.jonathansandell.expense_splitter.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final MemberRepository memberRepository;
    private final GroupService groupService;

    public ExpenseService(ExpenseRepository expenseRepository, MemberRepository memberRepository, GroupService groupService) {
        this.expenseRepository = expenseRepository;
        this.memberRepository = memberRepository;
        this.groupService = groupService;
    }

    public Expense createExpense(Long groupId, CreateExpenseRequest request) {
        Group group = groupService.getGroup(groupId);

        Member paidBy = memberRepository.findById(request.paidByMemberId())
                .orElseThrow(() -> new NoSuchElementException("No member with id " + request.paidByMemberId()));

        Set<Member> participants = new LinkedHashSet<>(memberRepository.findAllById(request.participantMemberIds()));
        if (participants.size() != request.participantMemberIds().size()) {
            throw new NoSuchElementException("One or more participant member ids do not exist");
        }

        Expense expense = new Expense(request.description(), request.amount(), group, paidBy, participants);
        return expenseRepository.save(expense);
    }

    public List<Expense> listExpenses(Long groupId) {
        return expenseRepository.findByGroupId(groupId);
    }
}
