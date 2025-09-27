// data/entity/User.kt
package com.example.database_project.entity
// User 엔티티 클래스가 속한 패키지 경로

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
// Room 관련 어노테이션과 직렬화를 위한 라이브러리 불러오기
// @Entity → DB 테이블 정의
// @Index → 특정 컬럼에 인덱스 생성
// @PrimaryKey → 기본키 지정
// @Serializable (현재는 사용하지 않았지만, 네트워크 전송/저장 시 객체 직렬화에 활용 가능)

@Entity(
    tableName = "users",
    indices = [Index(value = ["name"], unique = true)]
    // "users"라는 이름으로 DB에 테이블 생성
    // name 컬럼에 인덱스 추가 → 검색 속도 향상
    // unique = true → 동일한 이름을 가진 사용자는 중복 저장 불가
)
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // 사용자 고유 식별자 (PK)
    // autoGenerate = true → Room이 자동으로 id 값을 증가시켜 부여

    val name: String,
    // 사용자 이름 (필수값, Null 불가)

    val address: String
    // 사용자 주소 (필수값, Null 불가)
)
