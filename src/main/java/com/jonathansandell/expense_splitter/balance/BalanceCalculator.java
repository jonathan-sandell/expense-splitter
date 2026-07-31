package com.jonathansandell.expense_splitter.balance;

import com.jonathansandell.expense_splitter.entity.Expense;
import com.jonathansandell.expense_splitter.entity.Member;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes each member's net balance from a list of expenses. Plain Java,
 * no Spring/JPA runtime dependency beyond the entity getters - this is the
 * class that gets unit tested in isolation and later called from a service.
 *
 * Money is tracked in integer cents internally. Splitting an amount that
 * doesn't divide evenly among N participants (e.g. $10.00 / 3) leaves a
 * remainder; that remainder is handed out one cent at a time to participants
 * in iteration order, so the shares always sum to exactly the expense amount.
 * Without this, every uneven split would leave a stray fraction of a cent
 * unaccounted for, breaking the invariant that all balances sum to zero.
 */
public class BalanceCalculator {

    public List<MemberBalance> calculate(List<Expense> expenses) {
        // Keyed by the Member object itself, not member.getId(): ids are
        // database-assigned and null until persisted, so keying by id would
        // collide every not-yet-persisted member into the same bucket.
        // Reference equality is safe here because JPA's persistence-context
        // identity map guarantees the same DB row resolves to the same Java
        // object across every Expense in this list, as long as they were all
        // loaded within one session/transaction.
        Map<Member, Long> balanceCents = new LinkedHashMap<>();

        for (Expense expense : expenses) {
            long totalCents = toCents(expense.getAmount());
            List<Member> participants = expense.getParticipants().stream().toList();
            int participantCount = participants.size();

            long baseShareCents = totalCents / participantCount;
            long remainderCents = totalCents % participantCount;

            Member payer = expense.getPaidBy();
            balanceCents.merge(payer, totalCents, Long::sum);

            for (int i = 0; i < participantCount; i++) {
                Member participant = participants.get(i);
                long share = baseShareCents + (i < remainderCents ? 1 : 0);
                balanceCents.merge(participant, -share, Long::sum);
            }
        }

        return balanceCents.entrySet().stream()
                .map(entry -> new MemberBalance(entry.getKey(), fromCents(entry.getValue())))
                .toList();
    }

    private static long toCents(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private static BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }
}
