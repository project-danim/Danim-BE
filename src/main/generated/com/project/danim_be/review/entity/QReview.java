package com.project.danim_be.review.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReview is a Querydsl query type for Review
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReview extends EntityPathBase<Review> {

    private static final long serialVersionUID = 2043675660L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReview review = new QReview("review");

    public final com.project.danim_be.common.entity.QTimestamped _super = new com.project.danim_be.common.entity.QTimestamped(this);

    public final StringPath comment = createString("comment");

    //inherited
    public final DateTimePath<java.util.Date> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.project.danim_be.member.entity.QMember member;

    //inherited
    public final DateTimePath<java.util.Date> modifiedAt = _super.modifiedAt;

    public final NumberPath<Double> point = createNumber("point", Double.class);

    public final com.project.danim_be.post.entity.QPost post;

    public QReview(String variable) {
        this(Review.class, forVariable(variable), INITS);
    }

    public QReview(Path<? extends Review> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReview(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReview(PathMetadata metadata, PathInits inits) {
        this(Review.class, metadata, inits);
    }

    public QReview(Class<? extends Review> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new com.project.danim_be.member.entity.QMember(forProperty("member")) : null;
        this.post = inits.isInitialized("post") ? new com.project.danim_be.post.entity.QPost(forProperty("post"), inits.get("post")) : null;
    }

}

