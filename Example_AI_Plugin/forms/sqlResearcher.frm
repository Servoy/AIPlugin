{
    "customProperties":{
        "formComponent":false,
        "useCssPosition":true
    },
    "encapsulation":108,
    "items":[
        {
            "cssPosition":"20,calc(50% + 10px),-1,20,300,20",
            "json":{
                "cssPosition":{
                    "bottom":"-1",
                    "height":"20",
                    "left":"20",
                    "right":"calc(50% + 10px)",
                    "top":"20",
                    "width":"300"
                },
                "text":"Research Question",
                "toolTipText":"The question to research across the database"
            },
            "name":"label_question",
            "typeName":"bootstrapcomponents-label",
            "typeid":47,
            "uuid":"4438FFD0-4083-4528-9750-0D94AA6AD60D"
        },
        {
            "cssPosition":"40,calc(50% + 10px),-1,20,300,90",
            "json":{
                "cssPosition":{
                    "bottom":"-1",
                    "height":"90",
                    "left":"20",
                    "right":"calc(50% + 10px)",
                    "top":"40",
                    "width":"300"
                },
                "dataProviderID":"userMessage"
            },
            "name":"userMessage",
            "typeName":"bootstrapcomponents-textarea",
            "typeid":47,
            "uuid":"B8C7BD50-8A93-46B1-BA43-EE0E5C400494"
        },
        {
            "cssPosition":"138,calc(50% + 10px),-1,20,300,30",
            "json":{
                "cssPosition":{
                    "bottom":"-1",
                    "height":"30",
                    "left":"20",
                    "right":"calc(50% + 10px)",
                    "top":"138",
                    "width":"300"
                },
                "onActionMethodID":"6D05BB88-EF72-42BF-BDF6-D41D9854C7EE",
                "styleClass":"btn btn-primary",
                "text":"Research!"
            },
            "name":"button_research",
            "styleClass":"btn btn-primary",
            "typeName":"bootstrapcomponents-button",
            "typeid":47,
            "uuid":"08FFFEA4-DFCC-4A26-82B4-AD610383212D"
        },
        {
            "cssPosition":"176,calc(50% + 10px),-1,20,300,20",
            "json":{
                "cssPosition":{
                    "bottom":"-1",
                    "height":"20",
                    "left":"20",
                    "right":"calc(50% + 10px)",
                    "top":"176",
                    "width":"300"
                },
                "dataProviderID":"queryStatus"
            },
            "name":"queryStatus",
            "typeName":"bootstrapcomponents-datalabel",
            "typeid":47,
            "uuid":"EEAD2157-B7BA-4976-8A3E-D34A1BB0C125"
        },
        {
            "cssPosition":"206,calc(50% + 10px),-1,20,300,20",
            "json":{
                "cssPosition":{
                    "bottom":"-1",
                    "height":"20",
                    "left":"20",
                    "right":"calc(50% + 10px)",
                    "top":"206",
                    "width":"300"
                },
                "text":"Findings & Recommendations",
                "toolTipText":"The agent's summarized findings"
            },
            "name":"label_findings",
            "typeName":"bootstrapcomponents-label",
            "typeid":47,
            "uuid":"CE356B1F-3F6A-480D-82D7-462EF5AFA720"
        },
        {
            "cssPosition":"228,calc(50% + 10px),20,20,300,20",
            "json":{
                "cssPosition":{
                    "bottom":"20",
                    "height":"20",
                    "left":"20",
                    "right":"calc(50% + 10px)",
                    "top":"228",
                    "width":"300"
                },
                "dataProviderID":"answer"
            },
            "name":"answer",
            "typeName":"bootstrapcomponents-textarea",
            "typeid":47,
            "uuid":"01258721-7616-4CA8-B324-D608BEF8FA9C"
        },
        {
            "cssPosition":"20,20,-1,calc(50% + 10px),300,20",
            "json":{
                "cssPosition":{
                    "bottom":"-1",
                    "height":"20",
                    "left":"calc(50% + 10px)",
                    "right":"20",
                    "top":"20",
                    "width":"300"
                },
                "text":"Research Trace (skills & queries)",
                "toolTipText":"Skill loads, learnings saved, and every SQL statement the agent ran"
            },
            "name":"label_trace",
            "typeName":"bootstrapcomponents-label",
            "typeid":47,
            "uuid":"2860CE58-B9D0-47D6-8260-CA02316A22E5"
        },
        {
            "cssPosition":"40,20,calc(50% + 10px),calc(50% + 10px),300,200",
            "json":{
                "cssPosition":{
                    "bottom":"calc(50% + 10px)",
                    "height":"200",
                    "left":"calc(50% + 10px)",
                    "right":"20",
                    "top":"40",
                    "width":"300"
                },
                "dataProviderID":"sqlPlan",
                "styleClass":"form-control code-block"
            },
            "name":"sqlPlan",
            "styleClass":"form-control code-block",
            "typeName":"bootstrapcomponents-textarea",
            "typeid":47,
            "uuid":"764FE3F6-2FD9-4AEA-B29E-79551AEA6B8E"
        },
        {
            "cssPosition":"calc(50% + 20px),20,-1,calc(50% + 10px),300,20",
            "json":{
                "cssPosition":{
                    "bottom":"-1",
                    "height":"20",
                    "left":"calc(50% + 10px)",
                    "right":"20",
                    "top":"calc(50% + 20px)",
                    "width":"300"
                },
                "text":"Visualization",
                "toolTipText":"A chart supporting the findings"
            },
            "name":"label_visualization",
            "typeName":"bootstrapcomponents-label",
            "typeid":47,
            "uuid":"C8843D12-572A-47BD-A16C-AF1C585CD426"
        },
        {
            "cssPosition":"calc(50% + 45px),20,20,calc(50% + 10px),calc(50% - 30px),160",
            "json":{
                "cssPosition":{
                    "bottom":"20",
                    "height":"160",
                    "left":"calc(50% + 10px)",
                    "right":"20",
                    "top":"calc(50% + 45px)",
                    "width":"calc(50% - 30px)"
                }
            },
            "name":"chart",
            "typeName":"svychartjs-chart",
            "typeid":47,
            "uuid":"68E58306-0809-47CD-AAF8-F879272841DD"
        },
        {
            "height":480,
            "partType":5,
            "typeid":19,
            "uuid":"873AFDB1-4EC6-4106-B524-2DF0721435ED"
        }
    ],
    "name":"sqlResearcher",
    "navigatorID":"-1",
    "showInMenu":true,
    "size":"800,480",
    "typeid":3,
    "uuid":"7F0FC752-F8DA-4FEB-B137-C4F3F7C3E21B"
}
