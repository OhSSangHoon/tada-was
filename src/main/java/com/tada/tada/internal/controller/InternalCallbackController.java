package com.tada.tada.internal.controller;

/*
 * [담당: 상훈] — n8n 콜백 전용, 건드리지 말 것 (tada_directory_structure.md 참고)
 *
 * 지금은 비어있다. 스티커 생성 n8n 워크플로우가 Webhook -> ... -> Respond to Webhook
 * 구조의 "동기" 호출이라, n8n이 우리 서버로 별도 콜백을 보내는 상황이 아직 없다
 * (호출한 쪽이 같은 HTTP 요청의 응답으로 결과를 바로 받음).
 *
 * 나중에 비동기 워크플로우(n8n이 처리 끝나고 우리 서버로 먼저 알려주는 방식)가
 * 필요해지면 여기에 콜백 엔드포인트를 추가할 것.
 */
public class InternalCallbackController {

}
