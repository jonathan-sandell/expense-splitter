package com.jonathansandell.expense_splitter.service;

import com.jonathansandell.expense_splitter.balance.BalanceCalculator;
import com.jonathansandell.expense_splitter.balance.MemberBalance;
import com.jonathansandell.expense_splitter.balance.SettlementCalculator;
import com.jonathansandell.expense_splitter.balance.Transfer;
import com.jonathansandell.expense_splitter.entity.Expense;
import com.jonathansandell.expense_splitter.repository.ExpenseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Bridges the plain-Java balance/settlement calculators into the Spring
 * layer: loads a group's expenses and hands them to the calculators, which
 * stay framework-free on purpose.
 */
@Service
public class BalanceService {

    private final ExpenseRepository expenseRepository;
    private final GroupService groupService;
    private final BalanceCalculator balanceCalculator = new BalanceCalculator();
    private final SettlementCalculator settlementCalculator = new SettlementCalculator();

    public BalanceService(ExpenseRepository expenseRepository, GroupService groupService) {
        this.expenseRepository = expenseRepository;
        this.groupService = groupService;
    }

    public List<MemberBalance> getBalances(Long groupId) {
        groupService.getGroup(groupId);
        List<Expense> expenses = expenseRepository.findByGroupId(groupId);
        return balanceCalculator.calculate(expenses);
    }

    public List<Transfer> getSettlement(Long groupId) {
        return settlementCalculator.calculate(getBalances(groupId));
    }
}
