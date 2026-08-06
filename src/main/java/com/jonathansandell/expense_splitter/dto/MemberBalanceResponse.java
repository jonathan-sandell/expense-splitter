package com.jonathansandell.expense_splitter.dto;

import com.jonathansandell.expense_splitter.balance.MemberBalance;

import java.math.BigDecimal;

public record MemberBalanceResponse(Long memberId, String memberName, BigDecimal netBalance) {

    public static MemberBalanceResponse from(MemberBalance balance) {
        return new MemberBalanceResponse(
                balance.member().getId(),
                balance.member().getName(),
                balance.netBalance()
        );
    }
}
