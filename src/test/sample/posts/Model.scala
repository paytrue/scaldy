package sample.posts

import java.time.LocalDateTime

// from http://stackoverflow.com/questions/23832136/scala-domain-object-modeling

sealed trait Id {
  def strVal: String
}

case class UserId(strVal: String) extends Id

case class PostId(strVal: String) extends Id

trait Model {
  def id: Id

  def creationDate: java.time.LocalDateTime
}

case class User(
                 id: UserId,
                 creationDate: LocalDateTime,
                 name: String,
                 email: String
                 ) extends Model

trait Post extends Model {
  def id: PostId

  def user: User

  def title: String

  def body: String
}

trait Moderated {
  def isApproved: Boolean
}

case class UnModeratedPost(
                            id: PostId,
                            creationDate: LocalDateTime,
                            user: User,
                            title: String,
                            body: String
                            ) extends Post

case class ModeratedPost(
                          id: PostId,
                          creationDate: LocalDateTime,
                          user: User,
                          title: String,
                          body: String,
                          isApproved: Boolean
                          ) extends Post with Moderated

