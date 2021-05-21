import React, { useState, MouseEvent, ChangeEvent } from 'react';
import './App.css';
import SwaggerUI from "swagger-ui-react"
import "swagger-ui-react/swagger-ui.css"

function App() {

  let [showMask, setShowMask] = useState(false);

  function handleClick(e: MouseEvent<HTMLButtonElement>) {
    setShowMask(!showMask);
  }

  function handleFileChange(e: ChangeEvent<HTMLInputElement>) {
    let file = e.target.files?.item(0);
    let formData = new FormData();
    formData.append('json', file!);
    fetch("/swagger.json", {
      method: "POST",
      body: formData
    }).then(res => {
      if (200 === res.status) {
        window.location.reload();
      } else {
        alert("error. Check the console");  
        console.error('Error:', res);
      }
    }).catch(error => {
      alert("error. Check the console");
      console.error('Error:', error);
    })
  }

  let divs = [
    <button className="floating-action" onClick={handleClick}>更新</button>
  ];
  if (showMask) {
    divs.push(
      <div className="mask" onClick={() => setShowMask(false)}>
        <div className="menu" onClick={ e=> e.stopPropagation() }>
          <div className="line">
            <label>Update Swagger Json:</label>
            <input type="file" accept="application/json" onChange={handleFileChange} />
          </div>
        </div>
      </div>
    );
  }
  return (
    <div className="App">
      {divs}
      <SwaggerUI url="/swagger.json" />
    </div>
  );
}

export default App;
