# am-co-api
Code and instruction for amazing co api

# Selected tech stack

- Scala programming language: Less boilerplate, type inference, better collection api and more robust code.
- SBT for build tool
- Play Framework: lightweight web framework, build on Akka HTTP, with live reload, asynch and best practices build in for better productivity and greater results (No Magic, but has a learning curve)
- Play Json: chosen for fast and easy json handling.
- SBT-Native-Packager + docker plugin: Makes it easier to build docker images and work with docker images for Play Framework apps.
- Persistence: Scheduled task persisting to file every x minutes, can and should be improved, could be backed by a noSQL DB, redis or using Postgres LTree extension or based on path enumeration. Should also be able to bootstrap tree from a file.

# Architecture comments

Primary code can be found in file /app/controllers/AmazingCoController and routes are stated in conf/routes and task is in /app/tasks/TaskPersisterTask

- Tree build as Singleton (Object in scala) and in-memmory for fast look up and modification
- Trees internal data structure is a map for fast lookup of children and fast modification of nodes
- Each Node has a sequence of children ids for faster lookup of children and reduce time wasted when updating heights recursively. It will increase memory footprint, but performance should be better this way.
- Tree height is updated recursively for a node and its children based on parent height + 1, so by using top down this should be fast
- Routes are: GET  /api/v1/nodes/:id/children       
              POST /api/v1/nodes/:id/children       
              PUT  /api/v1/nodes/:id                
- Get returns children as json, Post adds a child (payload has to be {"nodeId": "something"}) and Put updates the parent for a given node to the payloads nodeId and recalculates heights for all children in that subtree


# Building and running the api

SBT can be installed from https://www.scala-sbt.org/

Run in dev mode with reload enabled on osx
- sudo mkdir /var/data
- cd /var/
- sudo chomod 777 data
- go back to root folder of project
- sbt clean compile ~run
- goto localhost:9000

Build docker image and push locally
- sbt clean compile dist
- sbt docker:publishLocal

Run docker image
- mkdir ~/data
- docker run -p 9000:9000 -v ~/data:/var/data amazing-co-api:latest

