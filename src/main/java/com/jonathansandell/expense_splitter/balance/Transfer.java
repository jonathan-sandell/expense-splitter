package com.jonathansandell.expense_splitter.balance;

import com.jonathansandell.expense_splitter.entity.Member;

import java.math.BigDecimal;

/**
 * One settlement payment: `from` owes `amount` to `to`. A list of these is
 * the minimal-ish set of payments that zeroes out every member's balance.
 */
public record Transfer(Member from, Member to, BigDecimal amount) {
}