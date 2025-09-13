/*
*Funcion para cargar las tareas
*/
function loadTasks() {
    const searchValue = document.getElementById("taskSearch").value;
    if(!searchValue){
        document.getElementById("filter").innerText = "All";
    }else{
        document.getElementById("filter").innerText = searchValue;
    }
    const value = document.getElementById("filter").innerText;
    console.log("VALUE: ",value);
    fetch("http://localhost:35000/task?name="+value,
        {
            method: "GET"
        })
        .then(res => res.json())
        .then(data => {
            let taskhtml = '';
            for(let i=0; i<data.length;i++){
                const task = data[i];
                taskhtml+= `
                <div class="task">
                    <h2>${task.name}</h2>   
                    <p>${task.description}</p>
                </div>`
            }
            document.getElementById("task-container").innerHTML = taskhtml;
        });
}

/*
*Funcion para añadir una tarea
*/
function addTask(){
    const taskName = document.getElementById("taskTitle").value;
    const description = document.getElementById("taskDescription").value;
    if (!taskName || !description) {
        alert('Please fill all the fields');
        return;
    }
    if(taskName.length > 20){
        alert('The title is too long');
        return;
    }
    if (description.length > 30) {
        alert('The description is too long');
        return;
    }
    fetch("http://localhost:35000/task",
        {
            headers: {
                "Accept": "application/json",
                "Content-Type": "application/json"
            },
            method: "POST",
            body: JSON.stringify({
                name: taskName,
                description: description,
            })
        })
        .then(res => res.json())
        .then(data => {
            console.log(data)
            const element = document.getElementById("task-information");
            element.classList.remove("hide");
            element.classList.add("show");
            let taskInfo = `
                <h3>Last task added: </h3>
                <ul>
                    <li><p>Id: ${data.id}</p></li>
                    <li><p>Name: ${data.name}</p></li>
                    <li><p>Description: ${data.description}</p></li>
                </ul>
            `
            element.innerHTML = taskInfo;
        });
    loadTasks();
}

window.addTask = addTask;
window.loadTasks = loadTasks;
document.addEventListener("DOMContentLoaded", function () {
    document.getElementById("filter").innerText = "All";
    loadTasks();
});
