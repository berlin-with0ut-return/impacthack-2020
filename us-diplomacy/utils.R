library(leaflet)
library(maps)
library(rlist)

# leaflet(options = leafletOptions(minZoom = 0, maxZoom = 18))
# mapWorld = map("world", fill = TRUE, plot = FALSE)
# leaflet(data = mapWorld) %>% addProviderTiles(providers$CartoDB.DarkMatterNoLabels)
embassies <- read.csv("./full-embassies.csv")
# leaflet(data = embassies) %>% addProviderTiles(providers$CartoDB.DarkMatterNoLabels) %>%
#   addCircleMarkers(~Longitude, ~Latitude, popup = ~as.character(Mission), 
#                    label = ~as.character(Name), radius = 5, color = "cyan", 
#                    stroke = FALSE, fillOpacity = 0.8)

# returns a dataframe of connected countries for addPolylines to use
connectionsDF <- function(csList, startLat, startLon) {
  if (typeof(csList) == "character") {
    countries <- unlist(strsplit(csList, ", "))
  } else {
    countries <- csList
  }
  linesDF <- data.frame(
    lat=double(),
    lon=double()
  )
  temp <- data.frame(startLat, startLon)
  names(temp) <- c("lat", "lon")
  linesDF <- rbind(linesDF, temp)
  for (country in countries) {
    countryInfo <- embassies[embassies$Country==country,3:4]
    countryLat <- countryInfo[1, 1]
    countryLon <- countryInfo[1, 2]
    temp <- data.frame(countryLat, countryLon)
    names(temp) <- c("lat", "lon")
    linesDF <- rbind(linesDF, temp)
  }
  # linesDF <- linesDF[order(linesDF$lon),]
  temp <- data.frame(lat=c(38.889248), lon=c(-77.050636))
  names(temp) <- c("lat", "lon")
  linesDF <- rbind(linesDF, temp)
  return(as.data.frame(linesDF[complete.cases(linesDF),]))
}

# grabs unique values from aggregated column, where each entry is a comma-separated list
uniqueListCol <- function(colVals) {
  allValues <- vector("list", 1000)
  i <- 1
  for (lst in colVals) {
    items <- unlist(strsplit(lst, ", "))
    for (it in items) {
      allValues[[i]] <- it
      i <- i + 1
    }
  }
  allValues <- unique(allValues)
  allValues <- Filter(Negate(is.null), allValues)
  return(allValues)
}

allCountries <- uniqueListCol(embassies$Country)
allTopics <- uniqueListCol(embassies$Topics)

# return a dataframe of countries relating to topic
topicCountries <- function(ctryTbl, topic) {
  relevant <- ctryTbl[grepl(topic, embassies$Topics),]
  return (relevant)
}

# returns the embassies dataframe filtered by country name and any connections the country has
connectedCountries <- function(countryName) {
  correctCountry <- embassies[embassies$Country == countryName,]
  allConnections <- uniqueListCol(correctCountry[,5])
  for (c in allConnections) {
    connCountryData <- embassies[embassies$Country == c,]
    correctCountry <- rbind(correctCountry, connCountryData)
  }
  return(correctCountry)
}

# csList <- embassies[1, 5]
# startLat <- embassies[1, 3]
# startLon <- embassies[1, 4]
# testAfg <- connectionsDF(csList, startLat, startLon)

# leaflet() %>% addProviderTiles(providers$CartoDB.DarkMatterNoLabels) %>%
#   addCircleMarkers(data = embassies, ~Longitude, ~Latitude, popup = ~as.character(Mission),
#                    label = ~as.character(Name), radius = 5, color = "cyan",
#                    stroke = FALSE, fillOpacity = 0.8) %>%
#   addPolylines(data = testAfg, lat = ~lat, lng= ~lon, weight = 2, color = "cyan")

# eduTest <- topicCountries("education")
# eduCountries <- c(eduTest$Country, uniqueListCol(eduTest$Connections))
# eduConnections <- connectionsDF(eduCountries, eduTest[1, 3], eduTest[1, 4])
# names(embassies)
# 
# leaflet() %>% addProviderTiles(providers$CartoDB.DarkMatterNoLabels) %>%
#   addCircleMarkers(data = embassies, ~Longitude, ~Latitude, popup = ~as.character(Mission),
#                    label = ~as.character(Property.Name), radius = 5, color = "cyan",
#                    stroke = FALSE, fillOpacity = 0.8) %>%
#   addPolylines(data = eduConnections, lat = ~lat, lng= ~lon, weight = 2, color = "darkblue")
