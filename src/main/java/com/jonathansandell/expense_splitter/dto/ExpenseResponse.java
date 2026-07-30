package com.jonathansandell.expense_splitter.dto;

import com.jonathansandell.expense_splitter.entity.Expense;
import com.jonathansandell.expense_splitter.entity.SplitType;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseResponse(
        Long id,
        String description,
        BigDecimal amount,
        Long groupId,
        Long paidByMemberId,
        List<Long> participantMemberIds,
        SplitType splitType
) {

    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getGroup().getId(),
                expense.getPaidBy().getId(),
                expense.getParticipants().stream().map(m -> m.getId()).toList(),
                expense.getSplitType()
        );
    }
}
