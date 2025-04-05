package com.example.substitutemanager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.ByteArrayInputStream
import java.io.InputStream
import org.junit.Assert.*
import android.content.res.AssetManager

/**
 * Test class for the SubstituteManager
 */
class SubstituteManagerTest {

    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor
    
    @Mock
    private lateinit var mockAssetManager: AssetManager

    private lateinit var substituteManager: SubstituteManager

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        
        // Setup mock SharedPreferences
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        
        // Setup mock AssetManager
        `when`(mockContext.assets).thenReturn(mockAssetManager)
        
        // Setup test data
        setupMockAssets()
        
        // Create the SubstituteManager with mocked context
        substituteManager = SubstituteManager(mockContext)
    }
    
    private fun setupMockAssets() {
        // Mock timetable CSV
        val timetableContent = """
            Day,Period,Teacher1,Teacher2,Teacher3,Teacher4,Teacher5
            Monday,1,John Smith,Jane Doe,Bob Johnson,,
            Monday,2,Jane Doe,John Smith,Alice Brown,,
            Tuesday,1,Bob Johnson,Alice Brown,John Smith,,
        """.trimIndent()
        val timetableStream = ByteArrayInputStream(timetableContent.toByteArray())
        
        // Mock substitutes CSV
        val substitutesContent = """
            Sarah Parker,+1234567890
            Michael Lee,+2345678901
            Lisa Taylor,+3456789012
        """.trimIndent()
        val substitutesStream = ByteArrayInputStream(substitutesContent.toByteArray())
        
        // Mock teachers JSON
        val teachersJson = """
            [
              {"name":"John Smith","phone":"+1111111111","isSubstitute":false},
              {"name":"Jane Doe","phone":"+2222222222","isSubstitute":false},
              {"name":"Bob Johnson","phone":"+3333333333","isSubstitute":false},
              {"name":"Alice Brown","phone":"+4444444444","isSubstitute":false},
              {"name":"Sarah Parker","phone":"+1234567890","isSubstitute":true},
              {"name":"Michael Lee","phone":"+2345678901","isSubstitute":true},
              {"name":"Lisa Taylor","phone":"+3456789012","isSubstitute":true}
            ]
        """.trimIndent()
        val teachersStream = ByteArrayInputStream(teachersJson.toByteArray())
        
        // Mock schedules JSON
        val schedulesJson = """
            {
              "john smith": [
                {"day":"monday","period":1,"className":"10A","originalTeacher":"john smith","substitute":""},
                {"day":"tuesday","period":1,"className":"9C","originalTeacher":"john smith","substitute":""}
              ],
              "jane doe": [
                {"day":"monday","period":2,"className":"10B","originalTeacher":"jane doe","substitute":""},
                {"day":"monday","period":1,"className":"9B","originalTeacher":"jane doe","substitute":""}
              ],
              "bob johnson": [
                {"day":"monday","period":1,"className":"9C","originalTeacher":"bob johnson","substitute":""},
                {"day":"tuesday","period":1,"className":"10A","originalTeacher":"bob johnson","substitute":""}
              ],
              "alice brown": [
                {"day":"monday","period":2,"className":"9C","originalTeacher":"alice brown","substitute":""},
                {"day":"tuesday","period":1,"className":"9B","originalTeacher":"alice brown","substitute":""}
              ]
            }
        """.trimIndent()
        val schedulesStream = ByteArrayInputStream(schedulesJson.toByteArray())
        
        // Mock assigned teachers JSON
        val assignedTeachersJson = """
            {
              "assignments": [],
              "warnings": []
            }
        """.trimIndent()
        val assignedTeachersStream = ByteArrayInputStream(assignedTeachersJson.toByteArray())
        
        // Setup the asset manager to return our streams
        `when`(mockAssetManager.open("data/timetable_file.csv")).thenReturn(timetableStream)
        `when`(mockAssetManager.open("data/Substitude_file.csv")).thenReturn(substitutesStream)
        `when`(mockAssetManager.open("data/total_teacher.json")).thenReturn(teachersStream)
        `when`(mockAssetManager.open("data/teacher_schedules.json")).thenReturn(schedulesStream)
        `when`(mockAssetManager.open("data/assigned_teacher.json")).thenReturn(assignedTeachersStream)
    }
    
    @Test
    fun testLoadData() = runBlocking {
        // Call loadData
        substituteManager.loadData()
        
        // Verify the assets were opened
        verify(mockAssetManager).open("data/timetable_file.csv")
        verify(mockAssetManager).open("data/Substitude_file.csv")
        verify(mockAssetManager).open("data/total_teacher.json")
        verify(mockAssetManager).open("data/teacher_schedules.json")
        verify(mockAssetManager).open("data/assigned_teacher.json")
    }
    
    @Test
    fun testAssignSubstitutes() = runBlocking {
        // Load data first
        substituteManager.loadData()
        
        // Test assigning substitutes for Jane Doe on Monday
        val assignments = substituteManager.assignSubstitutes("Jane Doe", "Monday")
        
        // Verify assignments were made
        assertFalse("Should return assignments", assignments.isEmpty())
        
        // Verify the assignments are saved
        verify(mockEditor).putString(anyString(), anyString())
    }
    
    @Test
    fun testVerifyAssignments() = runBlocking {
        // Load data first
        substituteManager.loadData()
        
        // Assign some substitutes
        substituteManager.assignSubstitutes("John Smith", "Monday")
        
        // Verify assignments
        val reports = substituteManager.verifyAssignments()
        
        // Check the reports
        assertEquals("Should return 3 reports", 3, reports.size)
    }
    
    @Test
    fun testClearAssignments() = runBlocking {
        // Load data first
        substituteManager.loadData()
        
        // Assign some substitutes
        substituteManager.assignSubstitutes("Bob Johnson", "Tuesday")
        
        // Clear assignments
        substituteManager.clearAssignments()
        
        // Get assignments and verify they're empty
        val assignments = substituteManager.getSubstituteAssignments()
        assertTrue("Assignments should be empty after clearing", assignments.isEmpty())
    }
}
