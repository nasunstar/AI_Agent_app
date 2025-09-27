package com.example.database_project.entity
// 이 파일이 entity 패키지에 포함됨 (DB 테이블 정의 모음)

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
// Room 관련 어노테이션 불러오기
// @Entity → 테이블 정의
// @ForeignKey → 다른 테이블과의 관계(FK)
// @PrimaryKey → 기본키 지정

@Entity(
    tableName = "event_notifications", // 실제 DB에서 생성될 테이블 이름
    foreignKeys = [
        ForeignKey(
            entity = Event::class,      // 참조할 부모 테이블 → Event
            parentColumns = ["id"],     // Event 테이블의 PK
            childColumns = ["eventId"], // EventNotification에서 FK로 가질 컬럼
            onDelete = ForeignKey.CASCADE // Event가 삭제되면 알림도 같이 삭제
        )
    ]
)
data class EventNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // EventNotification 자체의 고유 id (자동 증가)

    val eventId: Long,
    // FK: 메인 Event 테이블의 id 참조
    // 어떤 이벤트에 대한 알림인지 연결

    val notificationTime: Long,
    // 알림 발생 시각 (밀리초 단위, System.currentTimeMillis() 같은 값)
    // 예: 이벤트 시작 10분 전 같은 알림 스케줄링에 사용

    val status: String
    // 알림 상태를 문자열로 저장
    // 예: "pending" (대기 중), "sent" (발송됨), "canceled" (취소됨) 등
)
