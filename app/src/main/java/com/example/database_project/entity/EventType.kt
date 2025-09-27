package com.example.database_project.entity
// 이 파일이 entity 패키지 안에 속함 (DB 테이블 정의 클래스)

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
// Room 관련 어노테이션
// @Entity → 테이블 정의
// @Index → 특정 컬럼에 인덱스를 걸어 검색 성능 향상
// @PrimaryKey → 기본키(PK) 지정

@Entity(
    tableName = "event_types", // 실제 DB 테이블 이름
    indices = [Index(value = ["typeName"], unique = true)]
    // typeName 컬럼에 인덱스를 추가 → 검색 속도 향상
    // unique = true → 같은 typeName(중복된 이벤트 유형) 저장 불가
)
data class EventType(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // EventType 고유 id (자동 증가 PK)

    val typeName: String,
    // 이벤트의 기본 분류 (필수)
    // 예: "이메일", "결제", "회의", "문자"

    val subdivision: String?
    // 이벤트의 세부 분류 (선택)
    // 예: typeName = "이메일"일 때 subdivision = "업무", "개인"
    //     typeName = "결제"일 때 subdivision = "카드", "현금"
)
