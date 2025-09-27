// data/entity/Note.kt
package com.example.database_project.data.entity
// 이 파일은 data.entity 패키지에 포함됨 (DB 테이블 정의 모음)

import androidx.room.Entity
import androidx.room.PrimaryKey
// Room 관련 어노테이션
// @Entity → 테이블 정의
// @PrimaryKey → 기본키 지정

@Entity(tableName = "notes")   // ★ 반드시 Dao 쿼리(@Query)에서 사용하는 테이블명과 같아야 함
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // 기본키(PK): 각 Note를 구분하는 고유 id
    // autoGenerate = true → 새로운 데이터가 들어올 때마다 Room이 자동으로 id 증가시킴

    val title: String
    // Note의 제목(내용). 필수값(Null 불가)
)
