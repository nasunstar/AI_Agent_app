package com.example.database_project.entity
// 이 파일이 entity 패키지 안에 속함 (DB의 테이블 정의 모음)

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
// Room 관련 어노테이션
// @Entity → 테이블 정의
// @ForeignKey → 다른 테이블과의 관계(FK) 정의
// @Index → 검색 성능 향상 및 제약 조건 설정
// @PrimaryKey → 기본키 지정

@Entity(
    tableName = "event_details", // 실제 DB에서 생성될 테이블 이름
    foreignKeys = [
        ForeignKey(
            entity = Event::class,          // 외래키가 연결될 대상 엔티티
            parentColumns = ["id"],         // Event 테이블의 PK 컬럼
            childColumns = ["eventId"],     // EventDetail 테이블이 참조할 FK 컬럼
            onDelete = ForeignKey.CASCADE   // 부모(Event)가 삭제되면 자식(EventDetail)도 자동 삭제
        )
    ],
    indices = [Index(value = ["eventId"], unique = true)]
    // eventId에 인덱스를 걸어서 검색 속도 향상
    // unique = true → 하나의 이벤트에 대해 오직 하나의 상세정보만 가질 수 있도록 제약
)
data class EventDetail(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // EventDetail 자체의 고유 id (자동 증가)

    val eventId: Long,
    // FK: 메인 Event 테이블의 id를 참조
    // 어떤 이벤트의 상세정보인지 연결하는 역할

    val amount: Double?,
    // 금액 정보 (예: 결제 금액, 청구 금액 등)
    // 없을 수도 있으므로 nullable

    val location: String?,
    // 위치 정보 (예: 회의 장소, 매장 위치 등)
    // 없을 수도 있으므로 nullable

    val item: String?
    // 관련된 품목/아이템 정보 (예: 상품명, 회의 주제 등)
    // 없을 수도 있으므로 nullable
)
