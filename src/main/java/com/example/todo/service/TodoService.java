package com.example.todo.service;

import com.example.todo.config.JwtTokenProvider;
import com.example.todo.dto.PageResponseDto;
import com.example.todo.dto.TodoDto;
import com.example.todo.dto.TodoListDto;
import com.example.todo.entity.Todo;
import com.example.todo.entity.User;
import com.example.todo.exception.ErrorCode;
import com.example.todo.exception.CustomException;
import com.example.todo.repository.TodoRepository;
import com.example.todo.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional
public class TodoService {

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;


    @Transactional
    public TodoDto createTodo(TodoDto todoDto, HttpServletRequest request, HttpServletResponse response) {
        User user = getUserFromServlet(request);
        user = getUser(response, user);

        Todo todo = Todo.builder()
                .title(todoDto.getTitle())
                .user(user)
                .build();

        todoRepository.save(todo);
        return todoDto;
    }

    @Transactional(readOnly = true)
    public PageResponseDto findAll(int pageNo, int pageSize, String sortBy, HttpServletRequest request, HttpServletResponse response) {
        User user = getUserFromServlet(request);
        user = getUser(response, user);

        if (user.getTodoList().isEmpty()) {
            throw new CustomException(ErrorCode.TODO_NOT_FOUND);
        }

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(sortBy).descending());
        Page<Todo> todoPage = todoRepository.findAllByUser(pageable, user);

        List<Todo> todoList = todoPage.getContent();

        List<TodoListDto> content = todoList.stream()
                .map(todo -> TodoListDto.builder()
                        .title(todo.getTitle())
                        .completed(todo.getCompleted())
                        .build())
                .collect(Collectors.toList());


        return PageResponseDto.builder()
                .content(content)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalElements(todoPage.getTotalElements())
                .totalPages(todoPage.getTotalPages())
                .last(todoPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public TodoListDto findTodoById(Long id, HttpServletRequest request, HttpServletResponse response) {
        User user = getUserFromServlet(request);
        user = getUser(response, user);

        Todo todo = todoRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new CustomException(ErrorCode.TODO_NOT_FOUND));

        return TodoListDto.builder()
                .title(todo.getTitle())
                .completed(todo.getCompleted())
                .build();
    }

    @Transactional
    public TodoDto updateTodoById(Long id, TodoDto todoDto, HttpServletRequest request, HttpServletResponse response) {
        User user = getUserFromServlet(request);
        user = getUser(response, user);

        Todo todo = todoRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new CustomException(ErrorCode.TODO_NOT_FOUND));

        todo.setTitle(todoDto.getTitle());

        return TodoDto.builder()
                .title(todo.getTitle())
                .build();
    }

    @Transactional
    public void deleteTodoById(Long id, HttpServletRequest request, HttpServletResponse response) {
        User user = getUserFromServlet(request);
        user = getUser(response, user);

        Todo todo = todoRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new CustomException(ErrorCode.TODO_NOT_FOUND));

        todoRepository.delete(todo);
    }

    @Transactional
    public TodoListDto completeTodo(Long id, HttpServletRequest request, HttpServletResponse response) {
        User user = getUserFromServlet(request);
        user = getUser(response, user);

        Todo todo = todoRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new CustomException(ErrorCode.TODO_NOT_FOUND));

        complete(todo);

        return TodoListDto.builder()
                .title(todo.getTitle())
                .completed(todo.getCompleted())
                .build();
    }

    private void complete(Todo todo) {
        todo.setCompleted(true);
    }

    private User getUserFromServlet(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        return userRepository.findByUsername(username).orElse(null);
    }

    private User getUser(HttpServletResponse response, User user) {

        if (user == null) {
            String newAccessToken = response.getHeader("Authorization");
            String username = jwtTokenProvider.getUsernameFromToken(newAccessToken);

            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new CustomException(ErrorCode.TODO_NOT_FOUND));
        }

        return user;
    }
}