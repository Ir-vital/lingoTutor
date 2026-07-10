package com.example.data.model

data class VocabularyItem(
    val word: String,
    val definition: String,
    val translation: String,
    val example: String
)

data class Lesson(
    val id: String,
    val title: String,
    val category: String, // "Conversation", "Business", "Travel", "Vocabulary"
    val difficulty: String, // "Beginner", "Intermediate", "Advanced"
    val description: String,
    val objectives: List<String>,
    val xpReward: Int,
    val vocabulary: List<VocabularyItem>,
    val practiceTopic: String
)

object LessonData {
    val lessons = listOf(
        Lesson(
            id = "lesson_1",
            title = "Ordering at a Café",
            category = "Travel & Food",
            difficulty = "Beginner",
            description = "Learn how to politely order food and drinks in a coffee shop using standard everyday phrases.",
            objectives = listOf(
                "Greet the barista politely",
                "Order a beverage with custom options",
                "Handle payment and tip calculations"
            ),
            xpReward = 100,
            vocabulary = listOf(
                VocabularyItem("Beverage", "A drink of any type.", "Boisson", "Coffee is my favorite hot beverage."),
                VocabularyItem("Decaf", "Short for decaffeinated coffee.", "Décaféiné", "I prefer decaf coffee late in the evening."),
                VocabularyItem("Keep the change", "Tell the server to keep the remaining money as a tip.", "Gardez la monnaie", "The bill was $4.50, I gave $5 and told him to keep the change."),
                VocabularyItem("Pastry", "A sweet baked food made from flour, liquid, and fat.", "Pâtisserie", "They sell delicious fresh pastries at the counter.")
            ),
            practiceTopic = "Ordering a chocolate croissant and a large caramel macchiato at a busy London café."
        ),
        Lesson(
            id = "lesson_2",
            title = "The Job Interview",
            category = "Business English",
            difficulty = "Advanced",
            description = "Master professional English vocabulary to confidently describe your work accomplishments and career goals.",
            objectives = listOf(
                "Present your professional background concisely",
                "Explain complex skills and achievements",
                "Ask insightful questions to the recruiter"
            ),
            xpReward = 150,
            vocabulary = listOf(
                VocabularyItem("Accomplishment", "Something that has been achieved successfully.", "Réussite / Accomplissement", "Improving team sales by 20% was my biggest accomplishment."),
                VocabularyItem("Synergy", "The interaction of elements that when combined produce a total effect greater than the sum of their individual contributions.", "Synergie", "We must foster synergy between the design and development teams."),
                VocabularyItem("Proactive", "Taking action by causing change rather than reacting to events.", "Proactif", "A proactive approach helps prevent project delays."),
                VocabularyItem("Inquisitive", "Showing an interest in learning new things; curious.", "Curieux / Éveillé", "She has an inquisitive mind and loves solving programming bugs.")
            ),
            practiceTopic = "Simulating a job interview for a Software Engineer position at a dynamic tech startup."
        ),
        Lesson(
            id = "lesson_3",
            title = "Making Casual Friends",
            category = "Social Life",
            difficulty = "Beginner",
            description = "Learn casual icebreakers, how to ask about hobbies, and express common interests naturally.",
            objectives = listOf(
                "Start a conversation with an icebreaker",
                "Inquire about someone's hobbies",
                "Express shared excitement"
            ),
            xpReward = 100,
            vocabulary = listOf(
                VocabularyItem("Icebreaker", "A thing that is said or done to ease tension in social situations.", "Brise-glace", "Asking about the music playing is a great conversation icebreaker."),
                VocabularyItem("Hobbies", "Activities done in one's leisure time for pleasure.", "Loisirs", "My hobbies include playing guitar, reading, and hiking."),
                VocabularyItem("Shared interests", "Common hobbies or goals that people share.", "Intérêts communs", "We bonded quickly because we have many shared interests."),
                VocabularyItem("Stoked", "Slang for being extremely excited or pleased.", "Ravi / Hyper content", "I am stoked for the concert tonight!")
            ),
            practiceTopic = "Meeting a new neighbor at a neighborhood barbecue and finding common hobbies."
        ),
        Lesson(
            id = "lesson_4",
            title = "Asking for Directions",
            category = "Travel",
            difficulty = "Intermediate",
            description = "Confidently ask for directions in a foreign city and understand spatial prepositions.",
            objectives = listOf(
                "Ask for coordinates politely using 'Excuse me'",
                "Understand directions like 'turn left', 'go straight'",
                "Locate famous landmarks on a mental map"
            ),
            xpReward = 120,
            vocabulary = listOf(
                VocabularyItem("Landmark", "An object or feature of a landscape or town that is easily seen and recognized from a distance.", "Point de repère", "The Eiffel Tower is a global landmark."),
                VocabularyItem("Crosswalk", "A marked path where pedestrians can cross a street.", "Passage piéton", "Please look both ways before crossing at the crosswalk."),
                VocabularyItem("Go straight", "Continue walking or driving in the same direction without turning.", "Aller tout droit", "Go straight for two blocks, then turn left."),
                VocabularyItem("Polite", "Having or showing behavior that is respectful and considerate of other people.", "Poli", "It is polite to say 'Thank you' when someone gives you directions.")
            ),
            practiceTopic = "Asking an English bystander for directions to the British Museum in London."
        )
    )
}
