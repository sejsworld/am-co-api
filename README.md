# am-co-api
Code and instruction for amazing co api

# Selected tech stack

- Scala programming language: Less boilerplate, type inference, better collection api and more robust code.
- SBT for build tool
- Play Framework: lightweight web framework, build on Akka HTTP, with live reload, asynch and best practices build in for better productivity and greater results (No Magic, but has a learning curve)
- Play Json: chosen for fast and easy json handling.
- SBT-Native-Packager + docker plugin: Makes it easier to build docker images and work with docker images based for Play Framework
- Persistance: Scheduled task persisting to file, can and should be improved, could be backed by a noSQL DB, redis or using Postgres LTree extension or moddeled with path enumartion. Should also be able to bootstrap tree from a file.

# Architecture comments

Primary code can be found in file /app/controllers/AmazingCoController and routes are stated in conf/routes and task is in /app/tasks/TaskPersisterTask

- Tree build as Singleton (Object in scala) and in-memmory for fast look up and modifcation
- Trees internal datastructure is a map for fast lookup of children and fast modifcation of nodes
- Tree height is updated recursively for a node and its children based on parent height + 1, so by using top down this should be fast
- Routes are: GET  /api/v1/nodes/:id/children       
              POST /api/v1/nodes/:id/children       
              PUT  /api/v1/nodes/:id                
- Get returns children as json, Post adds a child (payload has to be {"nodeId": "something"}) and Put updates the parent for a given node to the payloads nodeId and recalculates heights for all children in that subtree


# Building and running the api

SBT can be installed from https://www.scala-sbt.org/

Run in dev mode with reload enabled on osx
- mkdir ~/data
- sbt clean compile ~run
- goto localhost:9000

Build docker image and push locally
- sbt clean compile dist
- sbt docker:publishLocal

Run docker image
- docker run -p 9000:9000 -v ~/data:/var/data amazing-co-api:latest

