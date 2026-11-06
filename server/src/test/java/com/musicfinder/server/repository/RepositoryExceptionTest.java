package com.musicfinder.server.repository;

import com.musicfinder.server.util.DataException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryExceptionTest {

    @Test
    void testDataExceptionPreservesCauseAndMessage() {
        SQLException sqlException = new SQLException("Connection refused");
        DataException exception = new DataException("Lỗi truy vấn CSDL khi tìm user test", sqlException);

        assertEquals("Lỗi truy vấn CSDL khi tìm user test", exception.getMessage());
        assertEquals(sqlException, exception.getCause());
        assertTrue(exception instanceof RuntimeException);
    }
}
