package com.jonathansandell.expense_splitter.controller;

import com.jonathansandell.expense_splitter.dto.MemberBalanceResponse;
import com.jonathansandell.expense_splitter.dto.TransferResponse;
import com.jonathansandell.expense_splitter.service.BalanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/balances")
    public List<MemberBalanceResponse> getBalances(@PathVariable Long groupId) {
        return balanceService.getBalances(groupId).stream().map(MemberBalanceResponse::from).toList();
    }

    @GetMapping("/settlement")
    public List<TransferResponse> getSettlement(@PathVariable Long groupId) {
        return balanceService.getSettlement(groupId).stream().map(TransferResponse::from).toList();
    }
}
