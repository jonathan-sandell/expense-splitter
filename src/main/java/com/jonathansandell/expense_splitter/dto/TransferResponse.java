package com.jonathansandell.expense_splitter.dto;

import com.jonathansandell.expense_splitter.balance.Transfer;

import java.math.BigDecimal;

public record TransferResponse(
        Long fromMemberId,
        String fromMemberName,
        Long toMemberId,
        String toMemberName,
        BigDecimal amount
) {

    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.from().getId(),
                transfer.from().getName(),
                transfer.to().getId(),
                transfer.to().getName(),
                transfer.amount()
        );
    }
}
