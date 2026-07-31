package com.jonathansandell.expense_splitter.balance;

import com.jonathansandell.expense_splitter.entity.Member;

import java.math.BigDecimal;

/**
 * A member's net position within a group: positive means the group owes
 * them money, negative means they owe the group money.
 */
public record MemberBalance(Member member, BigDecimal netBalance) {
}
