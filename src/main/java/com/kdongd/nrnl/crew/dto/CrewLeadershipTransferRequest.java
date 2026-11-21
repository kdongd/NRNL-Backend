package com.kdongd.nrnl.crew.dto;

import jakarta.validation.constraints.NotNull;

public record CrewLeadershipTransferRequest(@NotNull Long userId) {
}
