import React, { ChangeEvent, MouseEvent, useState } from 'react';
import './App.css';
import SwaggerUI from "swagger-ui-react"
import "swagger-ui-react/swagger-ui.css"

type ServiceName = string;

function App() {

  let [ currentService, setCurrentService ] = useState<string | undefined>(undefined);

  let [ services, setServices ] = useState<ServiceName[]>([ "test", "fan" ]);

  function handleAddService(e: MouseEvent<HTMLButtonElement>) {
    const serviceName = prompt("请输入名称");
    if (null == serviceName) {
      return;
    }
    fetch("/services", {
      method: "POST",
      body: serviceName
    }).then(res => {
      if (200 !== res.status) {
        alert("error. Check the console");
        console.error('Error:', res);
        return;
      }
      window.location.reload();
    });
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
    });
  }

  fetch("/services", {
    method: "GET"
  }).then(async res => {
    if (200 !== res.status) {
      alert("error. Check the console");
      console.error('Error:', res);
      return;
    }
    const text = await res.text();
    const services: ServiceName[] = JSON.parse(text);
    setServices(services);
    if (services.length > 0) {
      setCurrentService(services[0]);
    }
  });

  function selectOnChange(e: ChangeEvent<HTMLSelectElement>) {
    alert(e.target.value);
  }

  const serviceOptions = services.map(service => {
    return <option key={ service } value={ service }>{ service }</option>
  });

  let body = [];
  if (currentService) {
    body = [
      <fieldset>
        <div>
          <button onClick={ handleAddService }>新增</button>
        </div>
        <legend>服务信息</legend>
        <div>
          <select value={ currentService } onChange={ selectOnChange }>
            { serviceOptions }
          </select>
          &nbsp;&nbsp;&nbsp;&nbsp;
          <input type="file" accept="application/json" onChange={ handleFileChange }/>
        </div>
      </fieldset>,
      <SwaggerUI url={ "/" + currentService + ".json" }/>
    ]
  } else {
    body = [ <span>加载中</span> ]
  }
  return (
    <div className="App">
      { body }
    </div>
  );
}

export default App;
