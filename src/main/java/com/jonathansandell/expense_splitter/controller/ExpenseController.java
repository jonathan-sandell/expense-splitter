package com.jonathansandell.expense_splitter.controller;

import com.jonathansandell.expense_splitter.dto.CreateExpenseRequest;
import com.jonathansandell.expense_splitter.dto.ExpenseResponse;
import com.jonathansandell.expense_splitter.service.ExpenseService;
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
@RequestMapping("/api/groups/{groupId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse createExpense(@PathVariable Long groupId, @Valid @RequestBody CreateExpenseRequest request) {
        return ExpenseResponse.from(expenseService.createExpense(groupId, request));
    }

    @GetMapping
    public List<ExpenseResponse> listExpenses(@PathVariable Long groupId) {
        return expenseService.listExpenses(groupId).stream().map(ExpenseResponse::from).toList();
    }
}
