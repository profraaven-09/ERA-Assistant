package com.era.assistant

data class SubAgent(
    val id: String,
    val name: String,
    val role: String,
    val tools: List<String>
)

object EraAgentEngine {

    val activeAgents = mutableListOf<SubAgent>()

    fun processCommand(command: String, onResponse: (String, String) -> Unit) {
        val lower = command.lowercase()

        // 1. Device automation shortcuts
        when {
            "home" in lower -> {
                EraAccessibilityService.instance?.pressHome()
                onResponse("System", "Navigated to Home screen.")
                return
            }
            "back" in lower -> {
                EraAccessibilityService.instance?.pressBack()
                onResponse("System", "Navigated back.")
                return
            }
            "screenshot" in lower -> {
                EraAccessibilityService.instance?.takeScreenshot()
                onResponse("System", "Captured screen.")
                return
            }
            "notification" in lower -> {
                EraAccessibilityService.instance?.openNotifications()
                onResponse("System", "Notification shade opened.")
                return
            }
        }

        // 2. Dynamic Agent Spawning
        when {
            "deal" in lower || "price" in lower || "buy" in lower || "amazon" in lower || "सस्ता" in lower -> {
                val agent = SubAgent("AG_DEAL", "DealHunter", "E-Commerce Market Scanner", listOf("WebSearch", "PriceCompare"))
                activeAgents.add(agent)
                onResponse(agent.name, "DealHunter agent spawned. Scanning Amazon & Flipkart for the best prices and discounts...")
            }
            "travel" in lower || "ticket" in lower || "train" in lower || "flight" in lower || "होटल" in lower -> {
                val agent = SubAgent("AG_TRAVEL", "TravelVoyager", "Route & Hotel Planner", listOf("MapsAPI", "RailTracker"))
                activeAgents.add(agent)
                onResponse(agent.name, "TravelVoyager agent deployed. Planning itinerary, routes, and verifying hotel availability.")
            }
            "read" in lower || "pdf" in lower || "summary" in lower || "समरी" in lower -> {
                val agent = SubAgent("AG_DOC", "DocuSense", "Document & OCR Analyst", listOf("TextParser", "Summarizer"))
                activeAgents.add(agent)
                onResponse(agent.name, "DocuSense agent active. Reading document content and extracting key takeaways.")
            }
            else -> {
                val agent = SubAgent("AG_SOLVER", "TaskSolver", "General Execution Agent", listOf("WebQuery", "LocalFileManager"))
                activeAgents.add(agent)
                onResponse(agent.name, "TaskSolver sub-agent assigned: I am analyzing your request and executing the required workflow.")
            }
        }
    }
}
