package com.example.database_project.entity
// 패키지 경로 선언: 이 파일이 com.example.database_project.entity 패키지에 속함

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
// Room 라이브러리에서 엔티티, 외래키, 인덱스, 기본키 등을 정의하는 어노테이션 불러옴

@Entity(
    tableName = "events", // DB에 생성될 테이블 이름을 "events"로 지정
    // foreignKeys: 다른 테이블과 연결되는 관계(Foreign Key) 정의
    foreignKeys = [
        // userId → User 테이블의 id와 연결 (이벤트는 특정 사용자에 속함)
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"]),
        // eventTypeId → EventType 테이블의 id와 연결 (이벤트 종류: 이메일, 일정, 문자 등)
        ForeignKey(entity = EventType::class, parentColumns = ["id"], childColumns = ["eventTypeId"]),
        // contactId → Contact 테이블의 id와 연결 (이벤트가 특정 연락처와 관련 있을 수 있음)
        ForeignKey(entity = Contact::class, parentColumns = ["id"], childColumns = ["contactId"])
    ],
    // indices: 특정 컬럼에 인덱스를 걸어 검색 속도를 빠르게 함
    indices = [Index("userId"), Index("eventTypeId"), Index("contactId")]
)
data class Event(
    // @PrimaryKey: 테이블의 기본키 지정
    // autoGenerate = true → Room이 자동으로 ID를 1씩 증가시켜 부여
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // 이벤트 고유 식별자 (data_id)

    // 외래 키(FK) → User 테이블의 id와 연결
    val userId: Long, // 어떤 사용자의 이벤트인지 구분

    // 외래 키(FK) → EventType 테이블의 id와 연결
    val eventTypeId: Long, // 이벤트 종류 구분 (예: 메일, 결제, 회의 등)

    // 외래 키(FK) → Contact 테이블의 id와 연결
    // Nullable(?) → 이벤트가 연락처와 연결되지 않을 수도 있음
    val contactId: Long?, // 해당 이벤트와 관련된 연락처 ID (없으면 null)

    // 이벤트가 발생한 시간 (밀리초 단위, System.currentTimeMillis() 사용)
    val timestamp: Long, // event_timestamp

    // 원본 데이터(알림, 메일 전문, 메시지 내용 등)
    val originalContent: String, // 원문 텍스트

    // AI 요약이나 파싱된 결과를 저장하는 칼럼
    val summary: String, // 요약 텍스트

    // ⚠️ FloatArray 같은 배열이나 복잡한 객체는 Room이 직접 저장 불가
    // 따라서 TypeConverter를 만들어 JSON이나 String으로 변환해서 저장해야 함
    // (예: Embedding 벡터를 String으로 변환 후 저장)
    // 여기서는 예시라서 코드에 구현되지 않음
    // val embedding: FloatArray ← 추후 TypeConverter 필요
)
