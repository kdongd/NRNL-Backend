package com.kdongd.nrnl.exception;

public class InvalidCredentialsException extends RuntimeException{
    //파라미터 생성자가 없는 이유 : 이메일 또는 비밀번호가 틀린경우 구분 하지않고 하나의 메시지만 전송, 계정 존재 여부 노출X
    public InvalidCredentialsException() {
        super("이메일 또는 비밀번호가 올바르지 않습니다.");
    }
}
