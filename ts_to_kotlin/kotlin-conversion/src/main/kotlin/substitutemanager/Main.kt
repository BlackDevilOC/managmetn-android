package substitutemanager

import kotlinx.coroutines.runBlocking
import substitutemanager.models.SubstituteAssignment
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main entry point for the Substitute Manager application.
 * Provides both command-line arguments and interactive interface to interact with the SubstituteManager functionality.
 */
fun main(args: Array<String>) {
    println("Substitute Manager - Kotlin Implementation")
    println("=========================================")
    
    val manager = SubstituteManager()
    
    runBlocking {
        try {
            // Load data
            println("Loading data...")
            manager.loadData()
            println("Data loaded successfully.")
            
            // Check if command-line arguments are provided
            if (args.isNotEmpty()) {
                handleCommandLineArguments(args, manager)
                return@runBlocking
            }
            
            // Interactive command line interface
            try {
                val scanner = Scanner(System.`in`)
                var running = true
                
                while (running) {
                    println("\nAvailable commands:")
                    println("1. Assign substitutes for absent teacher")
                    println("2. View all assignments")
                    println("3. Clear assignments")
                    println("4. Verify assignments")
                    println("5. Exit")
                    print("\nEnter command (1-5): ")
                    
                    when (scanner.nextLine().trim()) {
                        "1" -> assignSubstitutesInteractive(scanner, manager)
                        "2" -> viewAllAssignments(manager)
                        "3" -> clearAssignments(manager)
                        "4" -> verifyAssignments(manager)
                        "5" -> {
                            running = false
                            println("Exiting...")
                        }
                        else -> println("Invalid command. Please enter a number between 1 and 5.")
                    }
                }
            } catch (e: NoSuchElementException) {
                println("Non-interactive environment detected. Use command-line arguments instead.")
                println("Usage: java -jar app.jar [command] [arguments...]")
                println("  Commands:")
                println("    assign <teacher_name> <day>  - Assign substitutes for absent teacher")
                println("    view                        - View all assignments")
                println("    clear                       - Clear all assignments")
                println("    verify                      - Verify assignments")
            }
            
        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
        }
    }
}

/**
 * Handle command-line arguments for non-interactive usage
 */
private fun handleCommandLineArguments(args: Array<String>, manager: SubstituteManager) {
    when (args[0].lowercase()) {
        "assign" -> {
            if (args.size < 3) {
                println("Error: Missing arguments for 'assign' command")
                println("Usage: assign <teacher_name> <day>")
                return
            }
            val teacherName = args[1]
            val day = args[2]
            println("Processing assignments for $teacherName on $day...")
            
            val assignments = manager.assignSubstitutes(teacherName, day)
            println("Created ${assignments.size} assignments:")
            assignments.forEach { assignment ->
                if (assignment is SubstituteAssignment) {
                    println("Period ${assignment.period}: ${assignment.substitute} for ${assignment.className}")
                }
            }
        }
        "view" -> viewAllAssignments(manager)
        "clear" -> clearAssignments(manager)
        "verify" -> verifyAssignments(manager)
        else -> {
            println("Error: Unknown command '${args[0]}'")
            println("Available commands: assign, view, clear, verify")
        }
    }
}

/**
 * Interactive assignment of substitutes
 */
private fun assignSubstitutesInteractive(scanner: Scanner, manager: SubstituteManager) {
    print("Enter absent teacher name: ")
    val teacherName = scanner.nextLine().trim()
    
    print("Enter date (YYYY-MM-DD) or press Enter for today: ")
    val dateInput = scanner.nextLine().trim()
    val date = if (dateInput.isEmpty()) {
        SimpleDateFormat("yyyy-MM-dd").format(Date())
    } else {
        dateInput
    }
    
    val day = SimpleDateFormat("EEEE").format(SimpleDateFormat("yyyy-MM-dd").parse(date))
    println("Processing assignments for $teacherName on $day...")
    
    val assignments = manager.assignSubstitutes(teacherName, day)
    println("Created ${assignments.size} assignments:")
    assignments.forEach { assignment ->
        if (assignment is SubstituteAssignment) {
            println("Period ${assignment.period}: ${assignment.substitute} for ${assignment.className}")
        }
    }
}

/**
 * View all current substitute assignments
 */
private fun viewAllAssignments(manager: SubstituteManager) {
    val response = manager.getSubstituteAssignments()
    val assignments = response["assignments"] as? List<*> ?: emptyList<Any>()
    
    if (assignments.isEmpty()) {
        println("No assignments found.")
    } else {
        println("Current assignments:")
        assignments.forEach { assignment ->
            if (assignment is SubstituteAssignment) {
                println("${assignment.originalTeacher} (Period ${assignment.period}, ${assignment.className}) -> ${assignment.substitute}")
            }
        }
    }
}

/**
 * Clear all substitute assignments
 */
private fun clearAssignments(manager: SubstituteManager) {
    println("Clearing all assignments...")
    manager.clearAssignments()
    println("Assignments cleared.")
}

/**
 * Verify the validity of current assignments
 */
private fun verifyAssignments(manager: SubstituteManager) {
    println("Verifying assignments...")
    val reports = manager.verifyAssignments()
    
    var allSuccess = true
    reports.forEach { report ->
        if (!report.success) {
            allSuccess = false
            println("\nVerification failed:")
            report.issues.forEach { println("- $it") }
        }
    }
    
    if (allSuccess) {
        println("All assignments verified successfully.")
    }
}
