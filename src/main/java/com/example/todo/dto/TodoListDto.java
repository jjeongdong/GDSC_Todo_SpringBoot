package com.example.todo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class TodoListDto {

    @NotBlank
    private String title;

    @NotBlank
    private Boolean completed;
}
