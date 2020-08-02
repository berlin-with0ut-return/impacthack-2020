library(shiny)
library(leaflet)
source("utils.R")

ui <- bootstrapPage(title="US Diplomacy Visualizer",
    tags$head(
        tags$style(HTML(".help-block {color: #FFFFFF !important;
                                      font-size: 20px;}")), 
        tags$style(type = "text/css", "html, body {width:150%;height:150%}"), 
        tags$style(HTML(
            "label {color: #FFFFFF;}",
        tags$title("US Diplomacy Visualizer")
        )),
        tags$style(
            "#errorMsg {
                color: #FFFFFF;
                font-size: 20px;
            }"
        )
    ),
    leafletOutput("map", width = "100%", height = "100%"),
    absolutePanel(top = 10, right = 10,
                  helpText("I want to learn about US diplomacy", color="cyan"),
                  selectInput("byCountry", "in: ",
                              c("", allCountries), selected = ""
                  ),
                  selectInput("byTopic", "relating to: ",
                              c("", allTopics), selected = ""
                  )
    ),
    absolutePanel(top = 10, left = 50, 
                  textOutput("errorMsg"))
)

server <- function(input, output, session) {
    circleData <- reactive({
        if (input$byCountry != "") {
            return(connectedCountries(input$byCountry))
        } else {
            return(embassies)
        }
    })
    
    polylinedata <- reactive({
        if (input$byCountry == "" & input$byTopic == "") {
            return(data.frame(lat=c(1, 1), lon=c(1, 1)))
        } else if (input$byCountry == "") {
            onTopic <- topicCountries(embassies, input$byTopic)
            allConnectedonTopic <- connectionsDF(uniqueListCol(onTopic$Connections), 
                                                 onTopic[1, 3], onTopic[1, 4])
            temp <- onTopic[, 3:4]
            names(temp) <- c("lat", "lon")
            topicConnections <- rbind(temp, allConnectedonTopic)
            return(topicConnections)
        } else if (input$byTopic == "") {
            correctCountry <- embassies[embassies$Country == input$byCountry,]
            countryConnections <- connectionsDF(uniqueListCol(correctCountry$Connections), 
                                                correctCountry[1, 3], correctCountry[1, 4])
            countryLatLon <- correctCountry[, 3:4]
            names(countryLatLon) <- c("lat", "lon")
            return(rbind(countryLatLon, countryConnections))
        } else {
            correctCountry <- embassies[embassies$Country == input$byCountry,]
            onTopicWithinCountry <- topicCountries(correctCountry, input$byTopic)
            connectCountryTopic <- connectionsDF(uniqueListCol(onTopicWithinCountry$Connections),
                                                 onTopicWithinCountry[1, 3],
                                                 onTopicWithinCountry[1, 4])
            countryLatLon <- onTopicWithinCountry[, 3:4]
            names(countryLatLon) <- c("lat", "lon")
            topicAndCountryFiltered <- rbind(countryLatLon, connectCountryTopic)
            return(topicAndCountryFiltered[complete.cases(topicAndCountryFiltered),])
        }
    })
    
    output$map <- renderLeaflet({
        leaflet() %>% addProviderTiles(providers$CartoDB.DarkMatterNoLabels) %>%
            setView(90, -50,  zoom = 3)
    })
    
    observe({
        leafletProxy("map") %>%
            clearShapes() %>%
            addPolylines(data = polylinedata(), lat = ~lat, lng= ~lon, weight = 2, color = "blue")
    })
    
    observe({
        leafletProxy("map") %>%
            clearMarkers() %>%
            addCircleMarkers(data = circleData(), ~Longitude, ~Latitude, popup = ~as.character(Mission),
                             label = ~as.character(Property.Name), radius = 5, color = "cyan",
                             stroke = FALSE, fillOpacity = 0.8)
    })
    
    output$errorMsg <- renderText({
        if (nrow(polylinedata()) == 1) {
            paste("Sorry, insufficient data")
        } 
    })

}
shinyApp(ui, server)