package com.formation.book.mapper;

import com.formation.book.dto.BookRequest;
import com.formation.book.dto.BookResponse;
import com.formation.book.model.Book;

public final class BookMapper {

    private BookMapper() {
    }

    public static BookResponse toResponse(Book book) {
        if (book == null) {
            return null;
        }
        return new BookResponse(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                book.getAvailableCopies()
        );
    }

    public static Book toEntity(BookRequest request) {
        return new Book(
                request.isbn(),
                request.title(),
                request.author(),
                request.availableCopies()
        );
    }
}
