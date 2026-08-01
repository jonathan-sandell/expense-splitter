package com.jonathansandell.expense_splitter.balance;

import com.jonathansandell.expense_splitter.entity.Member;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Turns a list of net balances into a list of transfers that settles them.
 * Greedy debt simplification: repeatedly match the largest creditor with the
 * largest debtor and settle as much of that pair as possible. This is not
 * the provably-minimal set of transactions (that variant is NP-hard, closer
 * to subset-sum), but greedy largest-to-largest matching gets close in
 * practice with a simple, defensible algorithm.
 *
 * Money is tracked in integer cents internally, same reasoning as
 * BalanceCalculator: avoids floating-point drift and keeps every settled
 * cent accounted for.
 */
public class SettlementCalculator {

    public List<Transfer> calculate(List<MemberBalance> balances) {
        PriorityQueue<Balance> creditors = new PriorityQueue<>(Comparator.comparingLong(Balance::cents).reversed());
        PriorityQueue<Balance> debtors = new PriorityQueue<>(Comparator.comparingLong(Balance::cents));

        for (MemberBalance balance : balances) {
            long cents = toCents(balance.netBalance());
            if (cents > 0) {
                creditors.add(new Balance(balance.member(), cents));
            } else if (cents < 0) {
                debtors.add(new Balance(balance.member(), cents));
            }
        }

        List<Transfer> transfers = new ArrayList<>();

        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Balance creditor = creditors.poll();
            Balance debtor = debtors.poll();

            long settledCents = Math.min(creditor.cents(), -debtor.cents());
            transfers.add(new Transfer(debtor.member(), creditor.member(), fromCents(settledCents)));

            long remainingCredit = creditor.cents() - settledCents;
            long remainingDebt = debtor.cents() + settledCents;

            if (remainingCredit > 0) {
                creditors.add(new Balance(creditor.member(), remainingCredit));
            }
            if (remainingDebt < 0) {
                debtors.add(new Balance(debtor.member(), remainingDebt));
            }
        }

        return transfers;
    }

    private record Balance(Member member, long cents) {
    }

    private static long toCents(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private static BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }
}