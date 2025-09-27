// ✅ 패키지 경로에서 '.data'를 삭제했습니다.
package com.example.database_project.dao
// 이 파일이 dao 패키지 안에 있음을 명시 (DAO = Data Access Object, DB 접근 인터페이스)

// Room 관련 어노테이션 불러오기
import androidx.room.*

// ✅ import 경로에서 '.data'를 삭제하여 올바른 Contact 클래스를 가리키도록 수정했습니다.
import com.example.database_project.entity.Contact
// Contact 테이블(Entity 클래스)을 불러옴

import kotlinx.coroutines.flow.Flow
// Flow: 코루틴 기반의 비동기 데이터 스트림 (DB 변경 시 자동으로 업데이트 받을 수 있음)

@Dao
interface ContactDao {
    // @Dao: 이 인터페이스가 Room DAO임을 선언
    // 여기서 정의한 메서드들은 Room이 내부적으로 SQL 코드로 변환해 실행함

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(contact: Contact): Long
    // 새로운 연락처(Contact) 데이터를 DB에 삽입
    // onConflict = IGNORE → name 컬럼(unique 인덱스 조건)이 충돌하면 무시
    // Long 반환 → 삽입된 행(row)의 id(PK)를 반환

    @Update
    suspend fun update(contact: Contact)
    // 기존 연락처 데이터를 수정
    // Contact 객체의 id(primary key)가 기준이 됨

    @Delete
    suspend fun delete(contact: Contact)
    // 기존 연락처 데이터를 삭제
    // Contact 객체의 id(primary key)가 기준이 됨

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Contact?
    // 특정 id를 가진 연락처(Contact) 1개만 가져오기
    // :id → 메서드 매개변수 id 값이 SQL에 바인딩됨
    // Contact? → 없을 경우 null 반환

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAll(): Flow<List<Contact>>
    // 모든 연락처를 가져오기 (이름 오름차순 정렬)
    // Flow<List<Contact>> → 비동기 스트림으로 반환
    // DB 변경 발생 시 자동으로 새로운 값 방출 (UI에 실시간 반영 가능)

    @Query("SELECT * FROM contacts WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): Contact?
}
// 특정 이름(name)을 가진
