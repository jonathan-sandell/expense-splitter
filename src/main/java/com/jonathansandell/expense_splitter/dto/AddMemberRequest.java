package com.jonathansandell.expense_splitter.dto;

import jakarta.validation.constraints.NotBlank;

public record AddMemberRequest(@NotBlank String name) {
}
