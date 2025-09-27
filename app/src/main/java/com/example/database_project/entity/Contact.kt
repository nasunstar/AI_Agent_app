package com.example.database_project.entity
// 이 파일이 entity 패키지에 속함을 나타냄 (DB의 테이블 정의 클래스들이 모여있는 곳)

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
// Room 라이브러리의 어노테이션을 가져옴
// @Entity → 테이블 정의
// @PrimaryKey → 기본키 지정
// @Index → 특정 컬럼에 인덱스를 걸어 검색 속도 향상

@Entity(
    tableName = "contacts", // 실제 DB에서 생성될 테이블 이름을 "contacts"로 지정
    indices = [Index(value = ["name"], unique = true)]
    // name 컬럼에 인덱스를 추가 → 이름으로 검색 시 성능 향상
    // unique = true → 동일한 이름을 가진 Contact는 중복 저장 불가
)
data class Contact(
    // 각 컬럼은 data class 프로퍼티로 정의됨

    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // id: 기본키(PK), 자동 증가(autoGenerate = true)
    // Room이 새로운 Contact가 추가될 때마다 1씩 증가된 id를 부여

    val name: String,
    // 필수값 (nullable 아님)
    // 연락처의 이름 (예: "홍길동")

    val email: String?,
    // 선택값 (nullable 가능)
    // 이메일 주소가 없을 수도 있으므로 '?' 처리

    val phoneNumber: String?
    // 선택값 (nullable 가능)
    // 전화번호가 없을 수도 있음
)
