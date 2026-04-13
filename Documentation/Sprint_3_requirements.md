#  **CSC 360 Sprint 3:** 

# **Game Viewer (V-SITS)**

SITS continues to dominate the simulation community.  With the success of remote clients, we are looking at a new age where we would like spectators to watch matches of tournaments in real time\! 

### 

### **Requirements**

Create a GUI program using JavaFX,called the **Viewer**, that allows users to:

* Run remotely from the Tournament server  
* View all of the Tournaments that are in the registration phase as well as all the tournaments that are running.  
* View all of the moves of each game in a tournament, as it is running.

To make Viewing a more interesting experience, add a setting to your tournament of how long to wait between moves in a game.  By default the value can be 0, but while a game is playing the setting should be 1 second (Look up Thread.sleep).

### **User Stories:**

* A user at the viewer selects a Server to connect to, using an IP and port number.  
* A user can see all of the tournaments in registration mode or tournaments that are presently running.  
* Note: If a new tournament is added while the user is logged in the user will not necessarily see the new tournament until the user reloads the page.  
* The User can select any running tournament.  If they do, the viewer will present all of the move data for that tournament.  The Viewer will present these moves in real time.  
* If the User leaves the view for a running tournament, they will no longer receive messages about that tournament unless they select that tournament again.

**Notes for Multithreading:**  
You may assume only one tournament is running at a time for testing purposes.  You may have multiple tournaments in registration mode.

### 

### **Deliverables**

**April 15:**  As a Squad: upload a full design document to moodle and bring a written document to class for the instructor.  For class, prepare a short presentation to discuss your design.  
**This design document should:**

* Review existing architecture that is relevant to the new design.  
* Discuss how the new architecture, in conjunction with the old architecture, will solve the problem.  
* Highlight any changes necessary to the existing architecture to make the system work.  For each change, explain if this change was inevitable, or if a different original design would have accommodated the change better.  
* Present the views necessary for this design.  (Views can be sketches or mocked up scenebuilder and shared among squadmates)

**April 22:**  As individuals: code, test, and debug your sprint, upload your repository link to Moodle.  Provide your self-assessment along with any notes.  
