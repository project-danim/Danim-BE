package com.project.danim_be.post.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPost is a Querydsl query type for Post
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPost extends EntityPathBase<Post> {

    private static final long serialVersionUID = -1319294116L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPost post = new QPost("post");

    public final com.project.danim_be.common.entity.QTimestamped _super = new com.project.danim_be.common.entity.QTimestamped(this);

    public final StringPath ageRange = createString("ageRange");

    public final com.project.danim_be.chat.entity.QChatRoom chatRoom;

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath gender = createString("gender");

    public final NumberPath<Integer> groupSize = createNumber("groupSize", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<Image, QImage> imageUrls = this.<Image, QImage>createList("imageUrls", Image.class, QImage.class, PathInits.DIRECT2);

    public final BooleanPath isDeleted = createBoolean("isDeleted");

    public final BooleanPath isRecruitmentEnd = createBoolean("isRecruitmentEnd");

    public final StringPath keyword = createString("keyword");

    public final StringPath location = createString("location");

    public final StringPath map = createString("map");

    public final com.project.danim_be.member.entity.QMember member;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final NumberPath<Integer> numberOfParticipants = createNumber("numberOfParticipants", Integer.class);

    public final StringPath postTitle = createString("postTitle");

    public final DatePath<java.time.LocalDate> recruitmentEndDate = createDate("recruitmentEndDate", java.time.LocalDate.class);

    public final DatePath<java.time.LocalDate> recruitmentStartDate = createDate("recruitmentStartDate", java.time.LocalDate.class);

    public final DatePath<java.time.LocalDate> tripEndDate = createDate("tripEndDate", java.time.LocalDate.class);

    public final DatePath<java.time.LocalDate> tripStartDate = createDate("tripStartDate", java.time.LocalDate.class);

    public QPost(String variable) {
        this(Post.class, forVariable(variable), INITS);
    }

    public QPost(Path<? extends Post> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPost(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPost(PathMetadata metadata, PathInits inits) {
        this(Post.class, metadata, inits);
    }

    public QPost(Class<? extends Post> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.chatRoom = inits.isInitialized("chatRoom") ? new com.project.danim_be.chat.entity.QChatRoom(forProperty("chatRoom"), inits.get("chatRoom")) : null;
        this.member = inits.isInitialized("member") ? new com.project.danim_be.member.entity.QMember(forProperty("member")) : null;
    }

}

