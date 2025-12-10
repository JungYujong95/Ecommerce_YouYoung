package org.example.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.domain.member.dto.request.SignUpRequest;
import org.example.domain.member.dto.response.MemberResponse;
import org.example.domain.member.service.MemberService;
import org.example.global.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

@Tag(name = "회원", description = "회원 관련 API")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이메일 중복")
    })
    @PostMapping("/signup")
    public ApiResponse<MemberResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        return ApiResponse.success(memberService.signUp(request));
    }

    @Operation(summary = "이메일 중복 확인", description = "이메일 중복 여부를 확인합니다")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/check-email")
    public ApiResponse<Boolean> checkEmail(
            @Parameter(description = "확인할 이메일", required = true)
            @RequestParam String email) {
        return ApiResponse.success(memberService.checkEmailDuplicate(email));
    }
}
